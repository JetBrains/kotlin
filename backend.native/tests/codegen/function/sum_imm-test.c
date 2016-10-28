extern void *resolve_symbol(const char*);

int
run_test() {
  int (*sum)(int) = resolve_symbol("kfun:sum");

  if (sum(2) != 35) return 1;

  return 0;
}
