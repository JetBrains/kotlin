extern void *resolve_symbol(const char*);

int
run_test() {
  int (*sum)(int, int) = resolve_symbol("kfun:sumFooBar");
  
  if (sum(2, 3) != 5) return 1;

  return 0;
}
