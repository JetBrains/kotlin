#!/bin/bash

clang -shared -o../out/production/MemLayout/libbridge.dylib bridge.c -I /System/Library/Frameworks/JavaVM.framework/Headers

