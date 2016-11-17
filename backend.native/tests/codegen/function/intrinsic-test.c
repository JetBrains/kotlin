extern void *resolve_symbol(const char*);

int
run_test() {
  int (*intrinsic)(int) = resolve_symbol("kfun:intrinsic(Int)");

  if (intrinsic(3) != 4) return 1;

  return 0;
}
