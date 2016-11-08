#include <stdint.h>
extern void *resolve_symbol(const char*);

int
run_test() {
  int (*eqeqB)(uint8_t, uint8_t) = resolve_symbol("kfun:eqeqB");
  int (*eqeqS)(int16_t, int16_t) = resolve_symbol("kfun:eqeqS");
  int (*eqeqI)(int    , int    ) = resolve_symbol("kfun:eqeqI");
  int (*eqeqL)(int64_t, int64_t) = resolve_symbol("kfun:eqeqL");
  int (*eqeqF)(float  , float  ) = resolve_symbol("kfun:eqeqF");
  int (*eqeqD)(double , double ) = resolve_symbol("kfun:eqeqD");
  if (!eqeqB(3   , 3   )) return 1;
  if (!eqeqS(3   , 3   )) return 1;
  if (!eqeqI(3   , 3   )) return 1;
  if (!eqeqL(3ll , 3ll )) return 1;
  if (!eqeqF(3.0f, 3.0f)) return 1;
  if (!eqeqD(3.0 , 3.0 )) return 1;

  return 0;
}
