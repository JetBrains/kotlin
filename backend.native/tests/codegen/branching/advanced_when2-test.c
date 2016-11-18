extern void *resolve_symbol(const char*);

int
run_test() {
  int (*advanced_when2)(int) = resolve_symbol("kfun:advanced_when2(Int)");

  if (advanced_when2(10) != 42) return 1;

  return 0;
}
