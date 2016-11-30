#ifdef __linux__
# define _GNU_SOURCE
#endif
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
    void* symbol = dlsym(RTLD_DEFAULT, name);

    if (!symbol) {
        printf("Could not find kotlin symbol '%s': %s\n", name, dlerror());
        exit(1);
    }

    return symbol;
}

int
kotlinNativeMain() {
#ifdef RUN_TEST
  void (*main)(void *) = resolve_symbol("kfun:main(Array<String>)");
  main((void *)0);
  return 0;
#else
  exit(run_test());
#endif
}

