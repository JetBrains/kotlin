#!/bin/bash

clang -shared -o../nativelib/libcallback.dylib callback.c -I /System/Library/Frameworks/JavaVM.framework/Headers -pthread

