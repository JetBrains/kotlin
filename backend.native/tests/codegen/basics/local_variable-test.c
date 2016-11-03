extern void *resolve_symbol(const char*);

int
run_test() {
  int (*local_variable)(int) = resolve_symbol("kfun:local_variable");

  if (local_variable(3) != 14) return 1;

  return 0;
}
