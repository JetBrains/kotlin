#include <stdint.h>
extern void *resolve_symbol(const char*);

int
run_test() {
  int (*eqeqB)(uint8_t, uint8_t) = resolve_symbol("kfun:eqeqB(Byte;Byte)");
  int (*eqeqS)(int16_t, int16_t) = resolve_symbol("kfun:eqeqS(Short;Short)");
  int (*eqeqI)(int    , int    ) = resolve_symbol("kfun:eqeqI(Int;Int)");
  int (*eqeqL)(int64_t, int64_t) = resolve_symbol("kfun:eqeqL(Long;Long)");
  int (*eqeqF)(float  , float  ) = resolve_symbol("kfun:eqeqF(Float;Float)");
  int (*eqeqD)(double , double ) = resolve_symbol("kfun:eqeqD(Double;Double)");
  void *(*helloString)()          = resolve_symbol("kfun:helloString()");
  void *(*goodbyeString)()        = resolve_symbol("kfun:goodbyeString()");
  int (*eqeqStr)(void *, void *) = resolve_symbol("kfun:eqeqStr(String;String)");

  int (*eqeqeq)(void *, void *)  = resolve_symbol("kfun:eqeqeq(Any?;Any?)");

  int (*gtI  )(int    , int    ) = resolve_symbol("kfun:gtI(Int;Int)");
  int (*ltI  )(int    , int    ) = resolve_symbol("kfun:ltI(Int;Int)");
  int (*geI  )(int    , int    ) = resolve_symbol("kfun:geI(Int;Int)");
  int (*leI  )(int    , int    ) = resolve_symbol("kfun:leI(Int;Int)");
  int (*neI  )(int    , int    ) = resolve_symbol("kfun:neI(Int;Int)");

  int (*gtF  )(float  , float  ) = resolve_symbol("kfun:gtF(Float;Float)");
  int (*ltF  )(float  , float  ) = resolve_symbol("kfun:ltF(Float;Float)");
  int (*geF  )(float  , float  ) = resolve_symbol("kfun:geF(Float;Float)");
  int (*leF  )(float  , float  ) = resolve_symbol("kfun:leF(Float;Float)");
  int (*neF  )(float  , float  ) = resolve_symbol("kfun:neF(Float;Float)");

  if (!eqeqB(3   , 3   )) return 1;
  if (!eqeqS(3   , 3   )) return 1;
  if (!eqeqI(3   , 3   )) return 1;
  if (!eqeqL(3ll , 3ll )) return 1;
  if (!eqeqF(3.0f, 3.0f)) return 1;
  if (!eqeqD(3.0 , 3.0 )) return 1;

  if (!eqeqStr(helloString(), helloString())) return 1;

  if (!eqeqeq(helloString(), helloString())) return 1;
  if (eqeqeq(helloString(), goodbyeString())) return 1;

  if (gtI   (2   , 3   )) return 1;
  if (ltI   (3   , 2   )) return 1;
  if (geI   (2   , 3   )) return 1;
  if (leI   (3   , 2   )) return 1;
  if (neI   (2   , 2   )) return 1;

  if (gtF   (2.0 , 3.0 )) return 1;
  if (ltF   (3.0 , 2.0 )) return 1;
  if (geF   (2.0 , 3.0 )) return 1;
  if (leF   (3.0 , 2.0 )) return 1;
  if (neF   (2.0 , 2.0 )) return 1;

  return 0;
}
