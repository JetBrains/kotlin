#include <stdint.h>

extern void *resolve_symbol(const char*);
extern void *AllocInstance(void *, int32_t);

#define CHECK_OBJ(obj, info) if ((uintptr_t)*(obj) != (uintptr_t)(info)) return 1;
#define MAGIC0 0xdeadbeef
#define MAGIC1 0xcafebabe
#define MAGIC2 0xabadbabe

int
run_test() {
  void *a_type_info = resolve_symbol("ktype:A");
  void *(*a_init)(void *) = resolve_symbol("kfun:A.<init>()");
  void *b_type_info = resolve_symbol("ktype:B");
  void *(*b_init)(void *, int) = resolve_symbol("kfun:B.<init>(Int)");
  int (*b_get_a)(void *) = resolve_symbol("kfun:B.<get-a>()");
  void *c_type_info = resolve_symbol("ktype:C");
  void *(*c_init)(void *, int, int) = resolve_symbol("kfun:C.<init>(Int;Int)");
  int (*c_get_a)(void *) = resolve_symbol("kfun:C.<get-a>()");

  void **a = (void **)AllocInstance(a_type_info, 1);
  CHECK_OBJ(a, a_type_info)

  void **b = (void **)AllocInstance(b_type_info, 1);
  CHECK_OBJ(b, b_type_info)
  void *b_obj = b_init((void *)b, MAGIC0);
  CHECK_OBJ(b, b_type_info)
  if (b_get_a(b_obj) != MAGIC0) return 1;


  void **c = (void **)AllocInstance(c_type_info, 1);
  CHECK_OBJ(c, c_type_info)
  void *c_obj = c_init((void *)c, 0xcafebabe, 0xabadbabe);
  CHECK_OBJ(c, c_type_info)
  if (c_get_a(c_obj) != MAGIC1) return 1;
  return 0;
}
