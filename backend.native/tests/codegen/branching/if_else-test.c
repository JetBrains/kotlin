extern void *resolve_symbol(const char*);

int
run_test() {
  int (*if_else)(int) = resolve_symbol("kfun:if_else");

  if (if_else(0) != 24) return 1;

  return 0;
}
