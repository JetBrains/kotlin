#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

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

// Raw reference to data, meaning T*, only for cleaness of intentions.
template <class T>
class RawRef {
 private:
  T* ptr_;
 public:
  RawRef(T* ptr) : ptr_(ptr) {}
  const T& get() const { return *ptr_; }
  void set(const T& value) { *ptr_ = value; }
};

// Object reference, adds reference counting in container. In real implementation
// we may want to differentiate between
template <class T>
class ObjRef {
 private:
  void* ptr_;

  explicit ObjRef(void* ptr) : ptr_(ptr) {
    if (ptr_) {
      container()->AddRef();
    }
  }

  T* ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<T*>(reinterpret_cast<uint8_t*>(ptr_) + sizeof(Container*));
  }

 public:
  ObjRef() : ptr_(nullptr) {}
  ObjRef(const ObjRef& other) : ptr_(nullptr) {
    Assign(other);
  }
  ObjRef& operator=(const ObjRef& other) {
    Assign(other);
    return *this;
  }

  ~ObjRef() {
    if (ptr_) {
      container()->Release();
    }
  }

  Container* container() {
    return *reinterpret_cast<Container**>(ptr_);
  }

  void Assign(const ObjRef<T>& other) {
    // TODO: optimize for an important case where containers match.
    if (ptr_) {
      container()->Release();
    }
    ptr_ = other.ptr_;
    if (ptr_) {
      container()->AddRef();
    }
  }

  template<typename M, int offset>
  RawRef<M> at() const {
    return RawRef<M>(
      reinterpret_cast<M*>(reinterpret_cast<uint8_t*>(ref()) + offset));
  }

  bool null() const { return ptr_ == nullptr; }

  static ObjRef<T> Alloc(Container* container) {
    return ObjRef<T>(container->Place(sizeof(T)));
  }
};

struct List {
  ObjRef<List> next_;
  int data_;
};

void test_placer() {
  printf("Start placement\n");
  Container heap(1024);
  constexpr int next_offset = offsetof(struct List, next_);
  constexpr int data_offset = offsetof(struct List, data_);
  {
    ObjRef<List> head = ObjRef<List>::Alloc(&heap);
    head.at<int, data_offset>().set(1);
    ObjRef<List> cur = head;
    for (int i = 0; i < 10; ++i) {
      cur.at<ObjRef<List>, next_offset>().set(ObjRef<List>::Alloc(&heap));
      cur = cur.at<ObjRef<List>, next_offset>().get();
      cur.at<int, data_offset>().set(i + 2);
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
