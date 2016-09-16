#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include <cassert>

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

template <class T>
class Ref {
 private:
  void* ptr_;

  explicit Ref(void* ptr) : ptr_(ptr) {
    if (ptr_) {
      container()->AddRef();
    }
  }

 public:
  Ref() : ptr_(nullptr) {}
  Ref(const Ref& other) : ptr_(nullptr) {
    Assign(other);
  }
  Ref& operator=(const Ref& other) {
    Assign(other);
    return *this;
  }

  ~Ref() {
    if (ptr_) {
      container()->Release();
    }
  }

  T* ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<T*>(reinterpret_cast<uint8_t*>(ptr_) + sizeof(Container*));
  }

  Container* container() {
    return *reinterpret_cast<Container**>(ptr_);
  }

  void Assign(const Ref<T>& other) {
    // TODO: optimize for an important case where containers matches.
    if (ptr_) {
      container()->Release();
    }
    ptr_ = other.ptr_;
    if (ptr_) {
      container()->AddRef();
    }
  }

  T* operator->() const {
    return ref();
  }

  bool null() const { return ptr_ == nullptr; }

  static Ref<T> Alloc(Container* container) {
    return Ref<T>(container->Place(sizeof(T)));
  }
};

struct List {
  Ref<List> next_;
  int data_;
};

void test_placer() {
  printf("Start placement\n");
  Container heap(1024);
  {
    Ref<List> head = Ref<List>::Alloc(&heap);
    head->data_ = 1;
    Ref<List> cur = head;
    for (int i = 0; i < 10; ++i) {
      cur->next_ = Ref<List>::Alloc(&heap);
      cur = cur->next_;
      cur->data_ = i + 2;
    }
    cur = head;
    while (!cur.null()) {
      printf("next is %d\n", cur->data_);
      cur = cur->next_;
    }
  }
  heap.Dispose();
}

int main() {
  test_placer();
  return 0;
}
