#!/bin/bash
set -e
repo="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"


"$repo/build.sh"
"$repo/loop.sh" 10000
