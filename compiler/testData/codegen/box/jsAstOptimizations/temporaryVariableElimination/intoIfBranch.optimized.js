function box() {
  var tmp = write();
  if (neverTrue()) {
    doSomething(tmp);
  }
  if (!(read() === 5))
    return 'write was eliminated';
  return 'OK';
}
