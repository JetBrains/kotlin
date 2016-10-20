#include <dlfcn.h>
#include <stdlib.h>
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
kotlinNativeMain() {
  return run_test();
}

int ktype_kotlin_any asm("_ktype:kotlin.Any");

#define DEFINE(name, symbol) int name() asm(#symbol);
#define DECLARE(name) \
int \
name() { \
  abort(); \
  return 1; \
}

#define DEFINE_AND_DECLARE(name, sym) \
  DEFINE(name, sym) \
  DECLARE(name)

DEFINE_AND_DECLARE(kfun_kotlin_any_to_string,_kfun:kotlin.Any.toString)
DEFINE_AND_DECLARE(kfun_kotlin_any_hash_code,_kfun:kotlin.Any.hashCode)
DEFINE_AND_DECLARE(kfun_kotlin_any_equals,_kfun:kotlin.Any.equals)
