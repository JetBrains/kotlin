#include <stdint.h>
extern void *resolve_symbol(const char*);

int
run_test() {
  int     (*sumIB)(int, uint8_t) = resolve_symbol("kfun:sumIB");
  int     (*sumIS)(int, int16_t) = resolve_symbol("kfun:sumIS");
  int     (*sumII)(int, int    ) = resolve_symbol("kfun:sumII");
  int64_t (*sumIL)(int, int64_t) = resolve_symbol("kfun:sumIL");
  float   (*sumIF)(int, float  ) = resolve_symbol("kfun:sumIF");
  double  (*sumID)(int, double ) = resolve_symbol("kfun:sumID");
  double  (*modID)(int, double ) = resolve_symbol("kfun:modID");
  if (sumIB(2, 3)    != 5)    return 1;
  if (sumIS(2, 3)    != 5)    return 1;
  if (sumII(2, 3)    != 5)    return 1;
  if (sumIL(2, 3l)   != 5l)   return 1;
  if (sumIF(2, 3.0f) != 5.0f) return 1;
  if (sumID(2, 3.0)  != 5.0)  return 1;
  if (modID(5, 3.0)  != 2.0)  return 1;

  return 0;
}
