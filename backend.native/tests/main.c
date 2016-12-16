#ifdef __linux__
# define _GNU_SOURCE
#endif
#include <dlfcn.h>
#include <stdlib.h>
#include <stdio.h>

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

extern void InitMemory();
extern void InitGlobalVariables();

int
main() {
  InitMemory();
  InitGlobalVariables();
  exit(run_test());
}
