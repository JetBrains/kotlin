extern void *resolve_symbol(const char*);

int
run_test() {
  int (*foo)(int, int) = resolve_symbol("kfun:foo(Int;Int)");
  
  if (foo(2, 3) != 5) return 1;

  return 0;
}
