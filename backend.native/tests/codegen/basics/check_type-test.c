#include <stdio.h>

extern void *resolve_symbol(const char*);

int
run_test() {
  int (*check_type)()      = resolve_symbol("kfun:check_type()");
  int (*check_not_type)()  = resolve_symbol("kfun:check_not_type()");
  int (*check_interface)() = resolve_symbol("kfun:check_interface()");

  if (!check_type())      return 1;
  if (!check_not_type())  return 1;
  if (!check_interface()) return 1;

  return 0;
}
