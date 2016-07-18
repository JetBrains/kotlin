#pragma once

#define STATIC_AREA_SIZE 1000
#define DYNAMIC_AREA_SIZE 1000

void init_dynamic_area();
void* malloc_static(int size);
void* malloc_dynamic(int size);


