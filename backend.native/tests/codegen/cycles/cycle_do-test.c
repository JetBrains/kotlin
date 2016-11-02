extern void *resolve_symbol(const char*);

int
run_test() {
  int (*cycle_do)(int) = resolve_symbol("kfun:cycle_do");

  if (cycle_do(3) != 5) return 1;
  if (cycle_do(0) != 3) return 1;

  return 0;
}
