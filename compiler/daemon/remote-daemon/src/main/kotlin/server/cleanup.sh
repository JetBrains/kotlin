#!/bin/bash

#
# Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

# Force delete directories script
# Usage: ./cleanup.sh [directory1] [directory2] ...

# Function to display usage
show_usage() {
    echo "Usage: $0 [directory1] [directory2] ..."
    echo "Example: $0 /tmp/old_files /var/log/old_logs"
    echo "Note: This will PERMANENTLY delete directories and all their contents!"
}

# Function to delete a directory
delete_directory() {
    local dir="$1"

    if [ ! -d "$dir" ]; then
        echo "Warning: Directory '$dir' does not exist"
        return 1
    fi

    echo "Deleting directory: $dir"

    if rm -rf "$dir"; then
        echo "Deleted: $dir"
        return 0
    else
        echo "Failed to delete: $dir"
        return 1
    fi
}

main() {
    # Check if no arguments provided
    if [ $# -eq 0 ]; then
        echo "Error: No directories specified"
        show_usage
        exit 1
    fi

    # Directly delete each directory without confirmation or summary counts
    for dir in "$@"; do
        delete_directory "$dir"
    done
}

main "$@"