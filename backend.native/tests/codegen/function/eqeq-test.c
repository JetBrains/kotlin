#include <stdint.h>
extern void *resolve_symbol(const char*);

int
run_test() {
  void *(*helloString)()          = resolve_symbol("kfun:helloString()");
  void *(*goodbyeString)()        = resolve_symbol("kfun:goodbyeString()");

  uint8_t (*eqeqB)(uint8_t, uint8_t) = resolve_symbol("kfun:eqeqB(Byte;Byte)");
  uint8_t (*eqeqS)(int16_t, int16_t) = resolve_symbol("kfun:eqeqS(Short;Short)");
  uint8_t (*eqeqI)(int    , int    ) = resolve_symbol("kfun:eqeqI(Int;Int)");
  uint8_t (*eqeqL)(int64_t, int64_t) = resolve_symbol("kfun:eqeqL(Long;Long)");
  uint8_t (*eqeqF)(float  , float  ) = resolve_symbol("kfun:eqeqF(Float;Float)");
  uint8_t (*eqeqD)(double , double ) = resolve_symbol("kfun:eqeqD(Double;Double)");
  uint8_t (*eqeqStr)(void *, void *) = resolve_symbol("kfun:eqeqStr(String;String)");
  uint8_t (*eqeqeq)(void *, void *)  = resolve_symbol("kfun:eqeqeq(Any?;Any?)");

  uint8_t (*gtI  )(int    , int    ) = resolve_symbol("kfun:gtI(Int;Int)");
  uint8_t (*ltI  )(int    , int    ) = resolve_symbol("kfun:ltI(Int;Int)");
  uint8_t (*geI  )(int    , int    ) = resolve_symbol("kfun:geI(Int;Int)");
  uint8_t (*leI  )(int    , int    ) = resolve_symbol("kfun:leI(Int;Int)");
  uint8_t (*neI  )(int    , int    ) = resolve_symbol("kfun:neI(Int;Int)");

  uint8_t (*gtF  )(float  , float  ) = resolve_symbol("kfun:gtF(Float;Float)");
  uint8_t (*ltF  )(float  , float  ) = resolve_symbol("kfun:ltF(Float;Float)");
  uint8_t (*geF  )(float  , float  ) = resolve_symbol("kfun:geF(Float;Float)");
  uint8_t (*leF  )(float  , float  ) = resolve_symbol("kfun:leF(Float;Float)");
  uint8_t (*neF  )(float  , float  ) = resolve_symbol("kfun:neF(Float;Float)");

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
