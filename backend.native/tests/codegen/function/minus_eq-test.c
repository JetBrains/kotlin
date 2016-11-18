extern void *resolve_symbol(const char*);

int
run_test() {
  int (*minus_eq)(int) = resolve_symbol("kfun:minus_eq(Int)");

  if (minus_eq(23) != -12) return 1;

  return 0;
}
