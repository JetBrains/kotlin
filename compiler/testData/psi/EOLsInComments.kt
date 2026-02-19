// COMPILATION_ERRORS

fun foo() {
  a
  + b
  a
  /** */+ b
  a
  /* */+ b
  a /*
  */  + b
  a
  /*
  */  + b
  a /**
  */  + b
  a //
  + b
  a //
+ b
}