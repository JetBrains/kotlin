extern void *resolve_symbol(const char*);

int
run_test() {
  int (*fortyTwo)() = resolve_symbol("kfun:fortyTwo()");

  if (fortyTwo() != 42) return 1;
  return 0;
}
