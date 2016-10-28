extern void *resolve_symbol(const char*);

int
run_test() {
  int (*when2)(int) = resolve_symbol("kfun:when2");

  if (when2(0) != 42) return 1;

  return 0;
}
