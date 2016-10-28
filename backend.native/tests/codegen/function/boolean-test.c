extern void *resolve_symbol(const char*);

int
run_test() {
  int (*bool_yes)() = resolve_symbol("kfun:bool_yes");

  if (!bool_yes()) return 1;

  return 0;
}
