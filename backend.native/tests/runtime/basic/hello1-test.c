extern void *resolve_symbol(const char*);

int
run_test() {
  void (*main)(void*) = resolve_symbol("kfun:main");
  
  main((void*)0);

  return 0;
}

