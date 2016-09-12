#include <stdlib.h>

extern int test_field_assignment_Int(int);

void  *malloc_heap() {
  return malloc(64);
}

int
main() {
  return test_field_assignment_Int(1);
}
