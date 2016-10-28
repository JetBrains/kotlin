extern void *resolve_symbol(const char*);

int
run_test() {
  int (*sum3)() = resolve_symbol("kfun:sum3");

  if (sum3() != 36) return 1;

  return 0;
}
