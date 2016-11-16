extern void *resolve_symbol(const char*);

int
run_test() {
  int (*castTest)() = resolve_symbol("kfun:castTest()");

  if (!castTest()) return 1;

  return 0;
}
