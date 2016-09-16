#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include <cassert>

struct Container {
  uint8_t* data_;
  uint8_t* current_;
  int size_;
  int ref_count_;

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
    if (current_ + size > data_ + size_) {
      return nullptr;
    }
    void* result = current_;
    current_ += size;
    return result;
  }

  void AddRef() {
    if (data_) {
      ref_count_++;
    }
  }

  void Release() {
    if (data_) {
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
struct Ref {
  T* ref_;
  Container* container_;

  Ref() : ref_(nullptr), container_(nullptr) {}
  Ref(const Ref& other) : ref_(nullptr), container_(nullptr) {
    Assign(other);
  }
  Ref(T* ref, Container* container) : ref_(ref), container_(container) {
    if (container_) {
      container_->AddRef();
    }
  }
  ~Ref() {
    if (ref_ && container_) {
      container_->Release();
    }
  }
  
  static Ref<T> Alloc(Container* container) {
    return Ref<T>(reinterpret_cast<T*>(container->Place(sizeof(T))), container);
  }

  void Assign(const Ref<T>& other) {
    if (ref_) {
      container_->Release();
    }
    container_ = other.container_;
    container_->AddRef();
    ref_ = other.ref_;
  }

   T* operator->() const {
     return ref_;
   }
  
  bool null() const { return ref_ == nullptr; } 
};

struct List {
  Ref<List> next_;
  int data_;
};

void test_placer() {
  printf("Start placement\n");
  Container heap(1024);
  
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

  heap.Dispose();
}

int main() {
  test_placer();
}
