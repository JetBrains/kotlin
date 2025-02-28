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
// COMPILATION_ERRORS
