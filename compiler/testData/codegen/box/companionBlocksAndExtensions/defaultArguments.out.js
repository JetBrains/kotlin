function C1$foo(x = 10) {
  return 'x=' + x;
}
function C2$foo(x) {
  C2$static_init();
  x = x === VOID ? get_state_0() : x;
  return 'x=' + x;
}
