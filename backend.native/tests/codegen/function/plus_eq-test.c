extern void *resolve_symbol(const char*);

int
run_test() {
  int (*plus_eq)(int) = resolve_symbol("kfun:plus_eq");

  if (plus_eq(3) != 14) return 1;

  return 0;
}
