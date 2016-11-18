extern void *resolve_symbol(const char*);

int
run_test() {
  int (*advanced_when5)(int) = resolve_symbol("kfun:advanced_when5(Int)");

  if (advanced_when5(5) != 24) return 1;

  return 0;
}
