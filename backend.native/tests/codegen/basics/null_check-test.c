#include <stdint.h>
#include <stdio.h>

extern void *resolve_symbol(const char*);

int
run_test() {
  uint8_t (*null_check_eqeq1  )() = resolve_symbol("kfun:null_check_eqeq1()");
  uint8_t (*null_check_eqeq2  )() = resolve_symbol("kfun:null_check_eqeq2()");
  uint8_t (*null_check_eqeqeq1)() = resolve_symbol("kfun:null_check_eqeqeq1()");
  uint8_t (*null_check_eqeqeq2)() = resolve_symbol("kfun:null_check_eqeqeq2()");

  if (null_check_eqeq1())    return 1;
  if (!null_check_eqeq2())   return 1;
  if (null_check_eqeqeq1())  return 1;
  if (!null_check_eqeqeq2()) return 1;

  return 0;
}
