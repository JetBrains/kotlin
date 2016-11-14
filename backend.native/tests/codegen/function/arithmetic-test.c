extern void *resolve_symbol(const char*);

int
run_test() {
  int (*square)(int) = resolve_symbol("kfun:square(Int)");
  int (*sum_of_squares)(int, int) = resolve_symbol("kfun:sumOfSquares(Int;Int)");
  int (*diff_of_squares)(int, int) = resolve_symbol("kfun:diffOfSquares(Int;Int)");
  int (*mod)(int, int) = resolve_symbol("kfun:mod(Int;Int)");
  int (*div)(int, int) = resolve_symbol("kfun:remainder(Int;Int)");
  
  if (square(2) != 4) return 1;
  if (sum_of_squares(2, 4) != 20) return 1;
  if (diff_of_squares(2, 4) != -12) return 1;
  if (mod(5, 2) != 2) return 1;
  if (div(5, 2) != 1) return 1;
  return 0;
}
