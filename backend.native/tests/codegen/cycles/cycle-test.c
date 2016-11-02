extern void *resolve_symbol(const char*);

int
run_test() {
  int (*cycle)(int) = resolve_symbol("kfun:cycle");

  if (cycle(1) != 2) return 1;
  if (cycle(0) != 1) return 1;

  return 0;
}
