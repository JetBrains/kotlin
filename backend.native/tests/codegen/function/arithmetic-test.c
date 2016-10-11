extern void *resolve_symbol(const char*);

int
run_test() {
  int (*square)(int) = resolve_symbol("kfun:square");
  int (*sum_of_squares)(int, int) = resolve_symbol("kfun:sumOfSquares");
  int (*diff_of_squares)(int, int) = resolve_symbol("kfun:diffOfSquares");
  int (*mod)(int, int) = resolve_symbol("kfun:mod");
  int (*div)(int, int) = resolve_symbol("kfun:remainder");
  
  if (square(2) != 4) return 1;
  if (sum_of_squares(2, 4) != 20) return 1;
  if (diff_of_squares(2, 4) != -12) return 1;
  if (mod(5, 2) != 2) return 1;
  if (div(5, 2) != 1) return 1;
  return 0;
}
