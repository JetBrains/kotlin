extern void *resolve_symbol(const char*);

int
run_test() {
  int (*when5)(int) = resolve_symbol("kfun:when5");

  if (when5(2) != 3) return 1;

  return 0;
}
