#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <cassert>
#include <cstddef>

#include "layout.h"

struct List {
  static const TypeInfo* GetTypeInfo() {
    const static TypeInfo result = {
      NameHash("List"), sizeof(List), nullptr, {0, 8},
      {}, // implemented interfaces
      {}, // methods
      {}  // fields
    };
    return &result;
  }

  ObjRef<List> next_;
  ObjRef<List> prev_;
  int data_;
};

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
