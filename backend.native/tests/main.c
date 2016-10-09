#include <dlfcn.h>
#include <stdio.h>
/**
 * > llc-mp-3.8 b.out -o b.S
 * > /opt/local/libexec/llvm-3.8/bin/clang main.c b.S -o sum-test
 */

extern int run_test();

void * resolve_symbol(char *name) {
  /* here we can add here some magic to resolve symbols in kotlin native*/
  return dlsym(RTLD_SELF, name);
}
    

int
main() {
  return run_test();
}
