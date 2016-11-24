extern void *resolve_symbol(const char*);

int
run_test() {
  int (*primary_constructor)(int, int) = resolve_symbol("kfun:primaryConstructorCall(Int;Int)");
  int (*secondary_constructor)(int) = resolve_symbol("kfun:secondaryConstructorCall(Int)");

  if (primary_constructor(0xdeadbeef, 41) != 42) return 1;

  if (secondary_constructor(41) != 42) return 1;
  return 0;
}
