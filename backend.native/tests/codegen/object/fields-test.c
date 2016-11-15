extern void *resolve_symbol(const char*);

int
run_test() {
  int (*globalTest)() = resolve_symbol("kfun:globalTest(Int)");
  int (*getGlobal)()  = resolve_symbol("kfun:<get-globalValue>()");

  if (getGlobal() != 1) return 1;
  if (globalTest(41) != 42) return 1;
  if (getGlobal() != 42) return 1;
  return 0;
}
