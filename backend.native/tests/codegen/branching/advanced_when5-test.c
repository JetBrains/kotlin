extern void *resolve_symbol(const char*);

int
run_test() {
  int (*advanced_when5)(int) = resolve_symbol("kfun:advanced_when5");

  if (advanced_when5(2) != 3) return 1;

  return 0;
}
