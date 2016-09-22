#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -jar $DIR/build/libs/translator-1.0.jar -I $DIR/kotstd/include $@
