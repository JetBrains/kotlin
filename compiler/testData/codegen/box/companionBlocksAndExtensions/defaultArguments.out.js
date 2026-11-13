function C1$foo(x) {
  x = x === VOID ? 10 : x;
  return 'x=' + x;
}
function C2$foo(x) {
  C2$static_init();
  x = x === VOID ? get_state_0() : x;
  return 'x=' + x;
}
