#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <cassert>
#include <cstddef>

class Container {
 private:
  uint8_t* data_;
  uint8_t* current_;
  int size_;
  int ref_count_;

 public:
  Container(int size)
    : size_(size), ref_count_(1) {
    data_ = reinterpret_cast<uint8_t*>(calloc(size_, 1));
    current_ = data_;
  }

  ~Container() {
    assert(ref_count_ == 0);
    free(data_);
  }

  void* Place(int size) {
    size += sizeof(Container*);
    if (current_ + size > data_ + size_) {
      return nullptr;
    }
    Container** result = reinterpret_cast<Container**>(current_);
    *result = this;
    current_ += size;
    return result;
  }

  void AddRef() {
    if (data_) {
      //printf("addref %d\n", ref_count_);
      ref_count_++;
    }
  }

  void Release() {
    if (data_) {
      // printf("release %d\n", ref_count_);
      ref_count_--;
    }
  }

  void Dispose() {
    // Destroy container ignoring non-zero refcount. Use with care.
    ref_count_ = 0;
    free(data_);
    data_ = nullptr;
    current_ = nullptr;
  }
};

// Raw reference to data, meaning T*, invented only for cleaness of intentions.
template <class T>
class RawRef {
 private:
  T* ptr_;
 public:
  RawRef(T* ptr) : ptr_(ptr) {}
  const T& get() const { return *ptr_; }
  void set(const T& value) { *ptr_ = value; }
};

class AnyObjRef {
 protected:
  void* ptr_;

  explicit AnyObjRef(void* ptr) : ptr_(ptr) {
    if (ptr_) {
      container()->AddRef();
    }
  }

 public:
  ~AnyObjRef() {
    if (ptr_) {
      container()->Release();
    }
  }

  Container* container() {
    return *reinterpret_cast<Container**>(ptr_);
  }

  template<typename M, int offset>
  RawRef<M> at() const {
    return RawRef<M>(
      reinterpret_cast<M*>(reinterpret_cast<uint8_t*>(any_ref()) + offset));
  }

  void Assign(const AnyObjRef& other) {
    // TODO: optimize for an important case where containers match?
    if (ptr_) {
      container()->Release();
    }
    ptr_ = other.ptr_;
    if (ptr_) {
      container()->AddRef();
    }
  }

  uint8_t* any_ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<uint8_t*>(ptr_) + sizeof(Container*);
  }

  AnyObjRef any_obj_at(int offset) const {
    assert(ptr_);
    return AnyObjRef(*reinterpret_cast<void**>(any_ref() + offset));
  }

  bool null() const { return ptr_ == nullptr; }
};

// Object reference, adds reference counting in container.
template <class T>
class ObjRef : public AnyObjRef {
 private:
  explicit ObjRef(void* ptr) : AnyObjRef(ptr) {}

  T* ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<T*>(any_ref());
  }

 public:
  ObjRef(const ObjRef& other) : AnyObjRef(nullptr) {
    Assign(other);
  }
  ObjRef& operator=(const ObjRef& other) {
    Assign(other);
    return *this;
  }

  void Assign(const ObjRef<T>& other) {
    AnyObjRef::Assign(other);
  }

  void CopyTo(ObjRef<T> other) const {
    assert(!other.null());
    if (ref()) {
      memcpy(other.ref(), ref(), sizeof(T));
      for (int i = 0; i < sizeof(T::obj_offsets) / sizeof(T::obj_offsets[0]); ++i) {
	AnyObjRef any = other.any_obj_at(T::obj_offsets[i]);
	if (!any.null()) {
	  any.container()->AddRef();
	}
      }
    } else {
      // TODO: shall we do anything if copy from/to null?
    }
  }

  ObjRef<T> Clone(Container* container) {
    ObjRef<T> result = Alloc(container);
    CopyTo(result);
    return result;
  }

  template<typename M, int offset>
  ObjRef<M> obj_at() const {
    return ObjRef<M>(
      reinterpret_cast<M*>(any_ref() + offset));
  }

  static ObjRef<T> Alloc(Container* container) {
    return ObjRef<T>(container->Place(sizeof(T)));
  }
};

struct List {
  ObjRef<List> next_;
  ObjRef<List> prev_;
  int data_;
  // Object offsets in the class, needed for CopyTo() operation to update refs and for GC.
  static constexpr int obj_offsets[] = { 0 /* =offsetof(struct List, next_) */, 8 /* =offsetof(struct List, prev_) */ };
};

constexpr int List::obj_offsets[];

void UpdateElement(ObjRef<List> element) {
  element.at<int, offsetof(struct List, data_)>().set
    (element.at<int, offsetof(struct List, data_)>().get() + 10);
}

void DoNotUpdateElement(ObjRef<List> element) {
  element.at<int, offsetof(struct List, data_)>().set
    (element.at<int, offsetof(struct List, data_)>().get() + 100);
}

constexpr int next_offset = offsetof(struct List, next_);
constexpr int data_offset = offsetof(struct List, data_);

void ReturnByValue(ObjRef<List> value) {
  value.at<int, data_offset>().set(239);
}

ObjRef<List> ReturnByRef(Container* container) {
  auto result = ObjRef<List>::Alloc(container);
  result.at<int, data_offset>().set(30);
  return result;
}

void test_placer() {
  Container heap(1024);
  {
    ObjRef<List> head = ObjRef<List>::Alloc(&heap);
    head.at<int, data_offset>().set(1);
    ObjRef<List> cur = head;
    for (int i = 0; i < 10; ++i) {
      cur.at<ObjRef<List>, next_offset>().set(ObjRef<List>::Alloc(&heap));
      cur = cur.at<ObjRef<List>, next_offset>().get();
      cur.at<int, data_offset>().set(i + 2);
    }

    // Pass by reference.
    cur = head;
    while (!cur.null()) {
      UpdateElement(cur);
      cur = cur.at<ObjRef<List>, next_offset>().get();
    }

    // Pass by value.
    cur = head;
    while (!cur.null()) {
      // We could place clone on stack as well.
      DoNotUpdateElement(cur.Clone(&heap));
      cur = cur.at<ObjRef<List>, next_offset>().get();
    }

    // Return by value is trivial in this system. CopyTo() into provided container.
    auto value = ObjRef<List>::Alloc(&heap);
    ReturnByValue(value);
    printf("By value is %d\n", value.at<int, data_offset>().get());

    // Return by references assumes passing of container where results to be allocated.
    value = ReturnByRef(&heap /* or stack container */);
    printf("By ref is %d\n", value.at<int, data_offset>().get());

    // Dump results.
    cur = head;
    // Pass by reference.
    while (!cur.null()) {
      UpdateElement(cur);
      cur = cur.at<ObjRef<List>, next_offset>().get();
    }
     // Pass by value.
    while (!cur.null()) {
      // We could place clone on stack as well.
      DoNotUpdateElement(cur.Clone(&heap));
      cur = cur.at<ObjRef<List>, next_offset>().get();
    }
    cur = head;
    while (!cur.null()) {
      printf("next is %d\n", cur.at<int, data_offset>().get());
      cur = cur.at<ObjRef<List>, next_offset>().get();
    }
  }
  heap.Dispose();
}

int main() {
  test_placer();
  return 0;
}
